import type { MockMethod } from 'vite-plugin-mock'

// First 5 words for review, next 5 as new — mirrors real today-plan logic
const reviewWords = [
  { id: 1, level_code: 'CET4', word: 'abandon',  ipa_uk: '/əˈbæn.dən/', ipa_us: '/əˈbæn.dən/', en_definition: 'To leave completely and permanently.', zh_definition: 'v. 放弃；抛弃', example_en: 'They had to abandon the project.', example_zh: '他们不得不放弃这个项目。', topic_code: 'ACTION', audio_url_uk: '', audio_url_us: '', emoji: '🚪', pos: 'v.' },
  { id: 2, level_code: 'CET4', word: 'abolish',  ipa_uk: '/əˈbɒl.ɪʃ/', ipa_us: '/əˈbɑː.lɪʃ/', en_definition: 'To officially end or stop something.', zh_definition: 'v. 废除；废止', example_en: 'They voted to abolish the death penalty.', example_zh: '他们投票废除死刑。', topic_code: 'LAW', audio_url_uk: '', audio_url_us: '', emoji: '⚖️', pos: 'v.' },
  { id: 3, level_code: 'CET4', word: 'absorb',   ipa_uk: '/əbˈzɔːb/', ipa_us: '/əbˈzɔːrb/', en_definition: 'To take in or soak up.', zh_definition: 'v. 吸收；吸引注意力', example_en: 'Plants absorb water through their roots.', example_zh: '植物通过根部吸收水分。', topic_code: 'SCIENCE', audio_url_uk: '', audio_url_us: '', emoji: '🌱', pos: 'v.' },
  { id: 4, level_code: 'CET4', word: 'abstract', ipa_uk: '/ˈæb.strækt/', ipa_us: '/ˈæb.strækt/', en_definition: 'Existing as an idea rather than a physical thing.', zh_definition: 'adj. 抽象的；n. 摘要', example_en: 'Love is an abstract concept.', example_zh: '爱是一个抽象的概念。', topic_code: 'ACADEMIC', audio_url_uk: '', audio_url_us: '', emoji: '💭', pos: 'adj.' },
  { id: 5, level_code: 'CET4', word: 'accelerate', ipa_uk: '/əkˈsel.ə.reɪt/', ipa_us: '/əkˈsel.ə.reɪt/', en_definition: 'To increase in speed.', zh_definition: 'v. 加速；促进', example_en: 'The car accelerated rapidly.', example_zh: '汽车迅速加速。', topic_code: 'MOTION', audio_url_uk: '', audio_url_us: '', emoji: '🏎️', pos: 'v.' },
]

const newWords = [
  { id: 6,  level_code: 'CET4', word: 'access',   ipa_uk: '/ˈæk.ses/', ipa_us: '/ˈæk.ses/', en_definition: 'The right or opportunity to use something.', zh_definition: 'n. 通道；使用权 v. 访问', example_en: 'Students have access to the library.', example_zh: '学生可以使用图书馆。', topic_code: 'DIGITAL', audio_url_uk: '', audio_url_us: '', emoji: '🔑', pos: 'n.' },
  { id: 7,  level_code: 'CET4', word: 'accurate', ipa_uk: '/ˈæk.jə.rət/', ipa_us: '/ˈæk.jɚ.ɪt/', en_definition: 'Correct in all details; exact.', zh_definition: 'adj. 准确的；精确的', example_en: 'The report is accurate.', example_zh: '这份报告是准确的。', topic_code: 'QUALITY', audio_url_uk: '', audio_url_us: '', emoji: '🎯', pos: 'adj.' },
  { id: 8,  level_code: 'CET4', word: 'achieve',  ipa_uk: '/əˈtʃiːv/', ipa_us: '/əˈtʃiːv/', en_definition: 'To succeed in doing something.', zh_definition: 'v. 实现；取得', example_en: 'She achieved her goals.', example_zh: '她实现了自己的目标。', topic_code: 'SUCCESS', audio_url_uk: '', audio_url_us: '', emoji: '🏆', pos: 'v.' },
  { id: 9,  level_code: 'CET4', word: 'acknowledge', ipa_uk: '/əkˈnɒl.ɪdʒ/', ipa_us: '/əkˈnɑː.lɪdʒ/', en_definition: 'To accept or admit the existence of something.', zh_definition: 'v. 承认；致谢', example_en: 'He acknowledged his mistake.', example_zh: '他承认了自己的错误。', topic_code: 'COMMUNICATION', audio_url_uk: '', audio_url_us: '', emoji: '👐', pos: 'v.' },
  { id: 10, level_code: 'CET4', word: 'acquire',  ipa_uk: '/əˈkwaɪər/', ipa_us: '/əˈkwaɪr/', en_definition: 'To get something, especially knowledge or a skill.', zh_definition: 'v. 获得；习得', example_en: 'She acquired a new skill.', example_zh: '她习得了一项新技能。', topic_code: 'LEARNING', audio_url_uk: '', audio_url_us: '', emoji: '📚', pos: 'v.' },
]

export default [
  {
    url: '/api/v1/study/today',
    method: 'get',
    response: () => ({
      code: 0, msg: 'ok',
      data: {
        date: new Date().toISOString().slice(0, 10),
        review_words: reviewWords,
        new_words: newWords,
        review_count: reviewWords.length,
        new_count: newWords.length,
        estimated_minutes: 8,
      },
    }),
  },
  {
    url: '/api/v1/study/answer',
    method: 'post',
    response: ({ body }: { body: Record<string, unknown> }) => {
      const stage = 2
      return {
        code: 0, msg: 'ok',
        data: {
          word_id: body.word_id,
          stage_before: stage,
          stage_after: body.result === 'correct' ? stage + 1 : Math.max(1, stage - 1),
          next_review_at: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
          mastered: false,
        },
      }
    },
  },
  {
    url: '/api/v1/study/answer-batch',
    method: 'post',
    response: ({ body }: { body: { answers: unknown[] } }) => ({
      code: 0, msg: 'ok',
      data: (body.answers || []).map((_: unknown, i: number) => ({
        word_id: i + 1, stage_before: 1, stage_after: 2, next_review_at: new Date().toISOString(), mastered: false, status: 'accepted',
      })),
    }),
  },
  {
    url: '/api/v1/study/reset',
    method: 'post',
    response: () => ({ code: 0, msg: 'ok', data: null }),
  },
  {
    url: '/api/v1/study/mark-mastered',
    method: 'post',
    response: () => ({ code: 0, msg: 'ok', data: null }),
  },
] as MockMethod[]
