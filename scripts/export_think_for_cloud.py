#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把本地 dev DB 的 Think 词库数据导出成可移植 SQL，供云端 import。
- word_bank(THINK_*) / level(THINK_*) / word_list(builtin) / word_list_item
- word_list_item.word_id 用自然键(level_code+word_lower)子查询解析 → 云端自增 ID 自动对齐
- 全程幂等（NOT EXISTS 守卫），重跑安全。只 INSERT，不删改已有用户数据。
用法：python scripts/export_think_for_cloud.py > think_cloud_data.sql
然后：docker exec -i vocab-mysql mysql -uroot -p<pw> vocabmaster < think_cloud_data.sql
"""
import sys
import pymysql

DB = dict(host="127.0.0.1", port=3306, user="root", password="root",
          database="vocabmaster", charset="utf8mb4",
          cursorclass=pymysql.cursors.DictCursor)


def esc(v):
    if v is None:
        return "NULL"
    if isinstance(v, (int, float)):
        return str(v)
    s = str(v).replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
    return f"'{s}'"


def main():
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass
    conn = pymysql.connect(**DB)
    out = []
    out.append("SET NAMES utf8mb4;")
    out.append("SET FOREIGN_KEY_CHECKS=0;")

    with conn.cursor() as cur:
        # 1. level (THINK_*)
        cur.execute("SELECT * FROM level WHERE code LIKE 'THINK_%'")
        levels = cur.fetchall()
        out.append("\n-- level (THINK_*)")
        for r in levels:
            cols = ",".join(r.keys())
            vals = ",".join(esc(v) for v in r.values())
            out.append(f"INSERT IGNORE INTO level ({cols}) VALUES ({vals});")

        # 2. word_bank (THINK_*) —— 排除 id（云端自增），按 (level_code, word_lower) 幂等
        cur.execute("SELECT * FROM word_bank WHERE level_code LIKE 'THINK_%' AND deleted_at IS NULL")
        words = cur.fetchall()
        out.append(f"\n-- word_bank (THINK_*, {len(words)} rows)")
        for r in words:
            kv = [(k, v) for k, v in r.items() if k != "id"]
            cols = ",".join(k for k, _ in kv)
            vals = ",".join(esc(v) for _, v in kv)
            out.append(
                f"INSERT INTO word_bank ({cols}) SELECT {vals} FROM dual "
                f"WHERE NOT EXISTS (SELECT 1 FROM word_bank "
                f"WHERE level_code={esc(r['level_code'])} AND word_lower={esc(r['word_lower'])} "
                f"AND deleted_at IS NULL);"
            )

        # 3. word_list (builtin Think 词库) —— 排除 id（云端自增），按 name 幂等
        cur.execute("SELECT * FROM word_list WHERE source_type='builtin' AND deleted_at IS NULL")
        lists = cur.fetchall()
        out.append(f"\n-- word_list (builtin, {len(lists)} rows)")
        for r in lists:
            kv = [(k, v) for k, v in r.items() if k != "id"]
            cols = ",".join(k for k, _ in kv)
            vals = ",".join(esc(v) for _, v in kv)
            out.append(
                f"INSERT INTO word_list ({cols}) SELECT {vals} FROM dual "
                f"WHERE NOT EXISTS (SELECT 1 FROM word_list WHERE name={esc(r['name'])} AND deleted_at IS NULL);"
            )

        # 4. word_list_item —— 用自然键解析 list_id + word_id（云端 ID 自动对齐）
        cur.execute(
            "SELECT i.unit_no, i.page, i.sort_order, wl.origin_level_code AS level_code, "
            "wl.name AS list_name, wb.word_lower "
            "FROM word_list_item i "
            "JOIN word_list wl ON wl.id = i.list_id "
            "JOIN word_bank wb ON wb.id = i.word_id "
            "WHERE wl.source_type='builtin'"
        )
        items = cur.fetchall()
        out.append(f"\n-- word_list_item ({len(items)} rows, 自然键解析)")
        for it in items:
            out.append(
                f"INSERT INTO word_list_item (list_id, word_id, unit_no, page, sort_order) "
                f"SELECT wl.id, wb.id, {esc(it['unit_no'])}, {esc(it['page'])}, {esc(it['sort_order'])} "
                f"FROM word_list wl JOIN word_bank wb "
                f"ON wb.level_code = wl.origin_level_code "
                f"WHERE wl.name={esc(it['list_name'])} "
                f"AND wb.level_code={esc(it['level_code'])} "
                f"AND wb.word_lower={esc(it['word_lower'])} AND wb.deleted_at IS NULL "
                f"AND NOT EXISTS (SELECT 1 FROM word_list_item x "
                f"WHERE x.list_id=wl.id AND x.word_id=wb.id);"
            )

    conn.close()
    sys.stdout.write("\n".join(out) + "\n")
    sys.stderr.write(f"[OK] {len(levels)} levels, {len(words)} words, {len(lists)} lists, {len(items)} items\n")


if __name__ == "__main__":
    main()
