# 09 — 词库采集方案

## 词库来源

### 1. KET / PET / FCE / CAE（剑桥通用英语）

| 等级 | 来源 | 获取方式 |
|------|------|----------|
| KET (A2) | Cambridge English 官方《A2 Key Vocabulary List》 | 官方 PDF 公开下载 |
| PET (B1) | Cambridge English 官方《B1 Preliminary Vocabulary List》 | 同上 |
| FCE (B2) | Cambridge English《B2 First Vocabulary List》 | 官方网站 |
| CAE (C1) | Cambridge English《C1 Advanced Handbook》词表 | 官方 Handbook |

**注意**：剑桥官方词表仅列单词和词性，不含完整释义/例句。释义和例句需从辅助词典补充（见第 4 节）。

### 2. 国内学段（小学 / 初中 / 高中 / 四级 / 六级 / 专八）

| 等级 | 来源 |
|------|------|
| 小学 | 《义务教育英语课程标准》附录 5 词汇表（800 词左右） |
| 初中 | 《义务教育英语课程标准》三级词汇表（1600 词左右） |
| 高中 | 《普通高中英语课程标准》词汇表（3500 词） |
| 大学四级 (CET-4) | 全国大学英语四、六级考试委员会官方大纲词汇 |
| 大学六级 (CET-6) | 同上 |
| 专业八级 (TEM-8) | 《高校英语专业八级考试大纲》 |

教育部课标词表为公开资料，CET 词表官方大纲也公开发售。第三方电子版需注意版权。

### 3. 辅助词表（用于补全释义、例句、音标、词频）

| 资源 | 用途 | 许可 |
|------|------|------|
| CMU Pronouncing Dictionary | 音标（ARPAbet 格式，可转 IPA） | 公有领域 |
| Wiktionary (英文维基词典) | 定义、例句、词源、变形 | CC BY-SA 3.0 |
| WordNet 3.1 | 词义分类、同近义词、反义词 | WordNet License（可商用） |
| COCA / BNC 词频表 | 词频分（frequency 字段） | 部分免费可下载 |
| ECDICT (开源电英汉词典) | 英汉释义 + 考纲标记 + 词频 | MIT License（非常友好） |

**推荐首选 ECDICT**（GitHub: `skywind3000/ECDICT`）—— 它已经把 CET4/CET6/专四/专八/考研/GRE/IELTS/TOEFL 等词表整合，包含英汉释义、词性、音标、BNC/COCA 频率，MIT 协议商用无忧。这是中文词库项目最省心的起点。

### 4. 音频

| 来源 | URL 模式 | 备注 |
|------|----------|------|
| 有道词典 | `https://dict.youdao.com/dictvoice?audio={word}&type={1 英式 / 2 美式}` | 未公开承诺可商用，谨慎使用，**建议生产环境下载回自己 CDN** |
| Cambridge Dictionary | 需抓取页面获取音频 URL | 不建议热链接，版权敏感 |
| Forvo | 真人发音，多口音 | 免费 API 需申请，有配额 |
| 自录 + TTS 合成 | 用 Azure Speech / Amazon Polly / Google Cloud TTS 批量生成 | 完全可控，约 $16 / 百万字符 |

**生产方案**：初期用有道词典（开发阶段），正式上线前用 Azure TTS 批量生成英美双音频存自己 OSS，后续所有单词音频都走自家 CDN。

### 5. 配图

| 来源 | 方式 | 许可 |
|------|------|------|
| Unsplash API | 按单词搜图，取首张 | Unsplash License（可商用，建议注明来源） |
| Pixabay API | 同上 | Pixabay License（可商用） |
| Pexels API | 同上 | Pexels License（可商用） |
| Emoji 兜底 | 对应 word 映射 emoji | Unicode 可用 |

配图仅作辅助记忆，MVP 阶段可全部用 emoji（参考 `seed/words_sample.csv`），后期再异步补充真实图片。

## 数据清洗管道

推荐用 Python 脚本处理（即使后端是 Java，一次性的 ETL 用 Python 更快）。

### 目录

```
tools/word-etl/
├── pyproject.toml
├── data/
│   ├── raw/                         # 原始数据
│   │   ├── ecdict.csv               # 从 github 下的 ECDICT
│   │   ├── cmudict.txt
│   │   ├── ket_wordlist.pdf
│   │   └── ...
│   └── output/
│       └── words_by_level/
│           ├── KET.csv
│           ├── PET.csv
│           └── ...
├── scripts/
│   ├── 01_parse_ecdict.py           # 解析 ECDICT 主库
│   ├── 02_extract_level_lists.py    # 按等级筛选
│   ├── 03_merge_cmudict.py          # 合并 IPA
│   ├── 04_fetch_examples.py         # 补例句（Wiktionary / ChatGPT / Claude API）
│   ├── 05_generate_audio.py         # 批量合成音频（Azure TTS）
│   ├── 06_assign_topics.py          # 主题分类（规则 + LLM）
│   ├── 07_fetch_images.py           # Unsplash 搜图
│   └── 08_export_to_db.py           # 导入到 MySQL
└── README.md
```

### 清洗流程

```
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│ ECDICT CSV  │   │ CMU Dict    │   │ 课标/剑桥    │
│ (含中英释义) │   │ (音标)      │   │ 词表(白名单) │
└──────┬──────┘   └──────┬──────┘   └──────┬──────┘
       │                 │                 │
       └─────────┬───────┴─────────┬───────┘
                 │                 │
          ┌──────▼──────┐   ┌──────▼──────┐
          │ 按等级过滤   │   │ IPA 合并    │
          └──────┬──────┘   └──────┬──────┘
                 │                 │
                 └────────┬────────┘
                          │
                 ┌────────▼────────┐
                 │ 缺失例句检查      │
                 └────────┬────────┘
                          │
              ┌───────────┴───────────┐
              │                       │
     ┌────────▼────────┐      ┌──────▼──────┐
     │ 有例句：保留    │      │ 无例句：LLM 生成 │
     └────────┬────────┘      └──────┬───────┘
              │                      │
              └──────────┬───────────┘
                         │
                ┌────────▼────────┐
                │ 主题分类(LLM)    │
                └────────┬────────┘
                         │
                ┌────────▼────────┐
                │ 音频URL生成/音频合成 │
                └────────┬────────┘
                         │
                ┌────────▼────────┐
                │ 配图(可选/异步)  │
                └────────┬────────┘
                         │
                ┌────────▼────────┐
                │ 人工审核         │
                └────────┬────────┘
                         │
                ┌────────▼────────┐
                │ 导入 word_bank  │
                └─────────────────┘
```

### 关键脚本示例

#### `01_parse_ecdict.py`

```python
"""
ECDICT 字段：
word,phonetic,definition,translation,pos,collins,oxford,tag,bnc,frq,exchange,detail,audio

tag 字段包含: zk(中考) gk(高考) cet4 cet6 ky(考研) gre ielts toefl 
"""
import csv

LEVEL_MAP = {
    'zk': 'JUNIOR',      # 中考≈初中
    'gk': 'SENIOR',      # 高考≈高中
    'cet4': 'CET4',
    'cet6': 'CET6',
    'tem8': 'TEM8',
    'ielts': 'FCE',      # 简化映射，雅思≈FCE/CAE
    # KET/PET/FCE/CAE/PRIMARY 需从专门词表匹配
}

def parse_row(row):
    tags = (row.get('tag') or '').split()
    levels = [LEVEL_MAP[t] for t in tags if t in LEVEL_MAP]
    return {
        'word': row['word'].strip().lower(),
        'phonetic': row.get('phonetic', '').strip(),
        'translation': row.get('translation', '').strip(),
        'definition': row.get('definition', '').strip(),
        'pos': row.get('pos', '').strip(),
        'bnc': int(row['bnc'] or 0),
        'frq': int(row['frq'] or 0),
        'levels': levels,
    }

with open('data/raw/ecdict.csv', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        entry = parse_row(row)
        # ... 写入中间 JSON / 分级 CSV
```

#### `04_fetch_examples.py`（用 Claude API 补例句）

```python
"""
对于 ECDICT 中无例句的词，调用 Claude API 批量生成。
注意：批量处理、限流、结果人工抽检。
"""
import anthropic
import json

client = anthropic.Anthropic()

PROMPT_TEMPLATE = """请为单词 "{word}" ({pos}) 生成 1 个适合 {level} 等级学习者的英文例句和中文翻译。
要求：
1. 例句简洁（10-18 词），展示该单词的常见用法
2. 不使用生僻词
3. 中文翻译准确自然

只返回 JSON，不要其他文字：
{{
  "example_en": "...",
  "example_zh": "..."
}}"""

def fetch_example(word: str, pos: str, level: str) -> dict:
    msg = client.messages.create(
        model="claude-opus-4-7",
        max_tokens=300,
        messages=[{"role": "user", "content": PROMPT_TEMPLATE.format(word=word, pos=pos, level=level)}]
    )
    text = msg.content[0].text.strip()
    # 去掉可能的 ```json ... ``` 包裹
    text = text.removeprefix('```json').removesuffix('```').strip()
    return json.loads(text)
```

每生成 100 条存盘一次（断点续跑），全部完成后人工抽检 5% 质量。

#### `05_generate_audio.py`（Azure TTS 批量合成）

```python
"""
用 Azure Cognitive Services Speech SDK 批量生成英美两种口音音频。
存到 ./data/output/audio/{accent}/{word}.mp3
后上传到 OSS，URL 回写 word_bank 表。
"""
import azure.cognitiveservices.speech as speechsdk
from pathlib import Path

def synthesize(word: str, accent: str, output_path: Path):
    config = speechsdk.SpeechConfig(subscription=AZURE_KEY, region=AZURE_REGION)
    voice = 'en-GB-SoniaNeural' if accent == 'uk' else 'en-US-JennyNeural'
    config.speech_synthesis_voice_name = voice
    config.set_speech_synthesis_output_format(
        speechsdk.SpeechSynthesisOutputFormat.Audio48Khz192KBitRateMonoMp3
    )
    audio_config = speechsdk.audio.AudioOutputConfig(filename=str(output_path))
    synth = speechsdk.SpeechSynthesizer(speech_config=config, audio_config=audio_config)
    result = synth.speak_text_async(word).get()
    return result.reason == speechsdk.ResultReason.SynthesizingAudioCompleted
```

成本估算：50000 词 × 2 口音 × 平均 8 字符 = 80 万字符 ≈ $13。

#### `06_assign_topics.py`（主题分类）

可以用两种方案：

- **方案 A**：WordNet 的语义分类 + 人工规则映射到我们定义的 20 个主题
- **方案 B**：调用 Claude API 批量分类（更快但有成本）

```python
PROMPT = """请把以下单词归类到这些主题之一（只返回主题代码）：
FOOD/ANIMAL/PERSON/EMOTION/ACTION/DESCRIBE/ACADEMIC/BUSINESS/NATURE/COLOR/TIME/QUANTITY/THINKING/SOCIETY/HEALTH/ACHIEVEMENT/ABSTRACT/LANGUAGE/LIFE/WORK/OTHER

单词：{word}
释义：{definition}

只返回主题代码，不要其他内容。"""
```

### 数据质量规则

入库前必须满足：

1. `word`, `word_lower` 非空
2. `ipa_uk` 或 `ipa_us` 至少有一个非空
3. `zh_definition` 非空
4. `en_definition` 非空（可以和 zh_definition 二选一作为必须）
5. `level_code` 必须在 level 表中存在
6. `(level_code, word_lower)` 组合唯一
7. `frequency` 必须在 [0, 1] 范围
8. `difficulty` 必须在 [1, 10] 范围
9. 若 `audio_url_*` 非空，必须是 HTTPS URL
10. `example_en` 长度 5~200 字符（如果有）

### 审核状态机

```
audit_status:
  0 = 待审核（初次导入）
  1 = 审核通过（默认展示给用户）
 -1 = 已下架（被管理员下线，历史进度保留）
```

MVP 阶段可跳过审核，直接 `audit_status=1`；有余力时管理后台加抽检流程。

## 初始种子数据

文件：`seed/words_sample.csv`（每级 20 词，共 200 词）

格式：

```csv
level_code,word,ipa_uk,ipa_us,zh_definition,en_definition,example_en,example_zh,topic_code,emoji,difficulty,frequency,pos
CET4,abandon,/əˈbændən/,/əˈbændən/,"v. 放弃；抛弃","To leave completely and permanently.","They had to abandon the project.","他们不得不放弃这个项目。",ACTION,🚪,5,0.72,v.
...
```

这套种子数据仅用于开发和演示，正式上线前应按上面的管道跑一次完整 ETL 生成完整词库。

## 导入数据库

```bash
# Java 侧：MyBatis-Plus 批量插入
mvn spring-boot:run -Dspring-boot.run.arguments=--app.import-seed=true

# 或直接用 mysql 命令导入（开发环境）
mysql -u vocab -p vocabmaster < seed/words_sample.sql
```

生产环境批量导入走管理后台 `POST /admin/words/import` 接口，上传 CSV。

## 后续迭代：LLM 辅助出题

有了基础词库后，可以把"生成拼写测试、选择题干扰项"也交给 LLM：

- 选择题四选一的干扰项，用同主题同难度的 3 个词作为干扰（语义接近但不相同）
- 听写模式的发音可以变速、变口音，增加难度

这些 MVP 不做，作为 v1.1 迭代方向。
