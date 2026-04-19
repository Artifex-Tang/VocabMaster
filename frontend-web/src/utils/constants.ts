export const LEVELS = [
  { code: 'KET',  nameZh: 'KET 剑桥入门',  nameEn: 'KET',   targetCount: 1500 },
  { code: 'PET',  nameZh: 'PET 剑桥初级',  nameEn: 'PET',   targetCount: 3500 },
  { code: 'FCE',  nameZh: 'FCE 剑桥中级',  nameEn: 'FCE',   targetCount: 5000 },
  { code: 'CAE',  nameZh: 'CAE 剑桥高级',  nameEn: 'CAE',   targetCount: 7500 },
  { code: 'PRIMARY',  nameZh: '小学',   nameEn: 'Primary',  targetCount: 800  },
  { code: 'JUNIOR',   nameZh: '初中',   nameEn: 'Junior',   targetCount: 2000 },
  { code: 'SENIOR',   nameZh: '高中',   nameEn: 'Senior',   targetCount: 3500 },
  { code: 'CET4', nameZh: '大学四级',  nameEn: 'CET-4',  targetCount: 4500 },
  { code: 'CET6', nameZh: '大学六级',  nameEn: 'CET-6',  targetCount: 6000 },
  { code: 'TEM8', nameZh: '专业八级',  nameEn: 'TEM-8',  targetCount: 13000 },
] as const

export type LevelCode = typeof LEVELS[number]['code']

// 艾宾浩斯九阶段间隔（小时）
export const EBBINGHAUS_INTERVALS_HOURS = [
  5 / 60,  // stage 1→2：5 分钟
  0.5,     // stage 2→3：30 分钟
  12,      // stage 3→4：12 小时
  24,      // stage 4→5：1 天
  48,      // stage 5→6：2 天
  96,      // stage 6→7：4 天
  168,     // stage 7→8：7 天
  360,     // stage 8→9：15 天
  720,     // stage 9→掌握：30 天
] as const

export const STAGE_LABELS: Record<number, string> = {
  0: '未学过',
  1: '第1次',
  2: '第2次',
  3: '第3次',
  4: '第4次',
  5: '第5次',
  6: '第6次',
  7: '第7次',
  8: '第8次',
  9: '已掌握',
}

export const ERROR_CODES = {
  OK: 0,
  PARAM_INVALID: 10001,
  RATE_LIMIT: 10002,
  TOKEN_INVALID: 20001,
  TOKEN_EXPIRED: 20002,
  NO_PERMISSION: 20003,
  REFRESH_TOKEN_INVALID: 20004,
  ACCOUNT_EXISTS: 30001,
  CODE_WRONG: 30002,
  ACCOUNT_NOT_FOUND: 30003,
  PASSWORD_WRONG: 30004,
  ACCOUNT_DISABLED: 30005,
} as const
