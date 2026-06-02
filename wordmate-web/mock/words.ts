import type { MockMethod } from 'vite-plugin-mock'

// 20 sample words (CET4 level)
const words = [
  { id: 1,  level_code: 'CET4', word: 'abandon',   ipa_uk: '/əˈbæn.dən/', ipa_us: '/əˈbæn.dən/', en_definition: 'To leave completely and permanently.', zh_definition: 'v. 放弃；抛弃', example_en: 'They had to abandon the project.', example_zh: '他们不得不放弃这个项目。', topic_code: 'ACTION', audio_url_uk: '', audio_url_us: '', emoji: '🚪', pos: 'v.' },
  { id: 2,  level_code: 'CET4', word: 'abolish',   ipa_uk: '/əˈbɒl.ɪʃ/', ipa_us: '/əˈbɑː.lɪʃ/', en_definition: 'To officially end or stop something.', zh_definition: 'v. 废除；废止', example_en: 'They voted to abolish the death penalty.', example_zh: '他们投票废除死刑。', topic_code: 'LAW', audio_url_uk: '', audio_url_us: '', emoji: '⚖️', pos: 'v.' },
  { id: 3,  level_code: 'CET4', word: 'absorb',    ipa_uk: '/əbˈzɔːb/', ipa_us: '/əbˈzɔːrb/', en_definition: 'To take in or soak up.', zh_definition: 'v. 吸收；吸引注意力', example_en: 'Plants absorb water through their roots.', example_zh: '植物通过根部吸收水分。', topic_code: 'SCIENCE', audio_url_uk: '', audio_url_us: '', emoji: '🌱', pos: 'v.' },
  { id: 4,  level_code: 'CET4', word: 'abstract',  ipa_uk: '/ˈæb.strækt/', ipa_us: '/ˈæb.strækt/', en_definition: 'Existing as an idea rather than a physical thing.', zh_definition: 'adj. 抽象的；n. 摘要', example_en: 'Love is an abstract concept.', example_zh: '爱是一个抽象的概念。', topic_code: 'ACADEMIC', audio_url_uk: '', audio_url_us: '', emoji: '💭', pos: 'adj.' },
  { id: 5,  level_code: 'CET4', word: 'accelerate', ipa_uk: '/əkˈsel.ə.reɪt/', ipa_us: '/əkˈsel.ə.reɪt/', en_definition: 'To increase in speed.', zh_definition: 'v. 加速；促进', example_en: 'The car accelerated rapidly.', example_zh: '汽车迅速加速。', topic_code: 'MOTION', audio_url_uk: '', audio_url_us: '', emoji: '🏎️', pos: 'v.' },
  { id: 6,  level_code: 'CET4', word: 'access',    ipa_uk: '/ˈæk.ses/', ipa_us: '/ˈæk.ses/', en_definition: 'The right or opportunity to use something.', zh_definition: 'n. 通道；使用权 v. 访问', example_en: 'Students have access to the library.', example_zh: '学生可以使用图书馆。', topic_code: 'DIGITAL', audio_url_uk: '', audio_url_us: '', emoji: '🔑', pos: 'n.' },
  { id: 7,  level_code: 'CET4', word: 'accurate',  ipa_uk: '/ˈæk.jə.rət/', ipa_us: '/ˈæk.jɚ.ɪt/', en_definition: 'Correct in all details; exact.', zh_definition: 'adj. 准确的；精确的', example_en: 'The report is accurate.', example_zh: '这份报告是准确的。', topic_code: 'QUALITY', audio_url_uk: '', audio_url_us: '', emoji: '🎯', pos: 'adj.' },
  { id: 8,  level_code: 'CET4', word: 'achieve',   ipa_uk: '/əˈtʃiːv/', ipa_us: '/əˈtʃiːv/', en_definition: 'To succeed in doing something.', zh_definition: 'v. 实现；取得', example_en: 'She achieved her goals.', example_zh: '她实现了自己的目标。', topic_code: 'SUCCESS', audio_url_uk: '', audio_url_us: '', emoji: '🏆', pos: 'v.' },
  { id: 9,  level_code: 'CET4', word: 'acknowledge', ipa_uk: '/əkˈnɒl.ɪdʒ/', ipa_us: '/əkˈnɑː.lɪdʒ/', en_definition: 'To accept or admit the existence of something.', zh_definition: 'v. 承认；致谢', example_en: 'He acknowledged his mistake.', example_zh: '他承认了自己的错误。', topic_code: 'COMMUNICATION', audio_url_uk: '', audio_url_us: '', emoji: '👐', pos: 'v.' },
  { id: 10, level_code: 'CET4', word: 'acquire',   ipa_uk: '/əˈkwaɪər/', ipa_us: '/əˈkwaɪr/', en_definition: 'To get something, especially knowledge or a skill.', zh_definition: 'v. 获得；习得', example_en: 'She acquired a new skill.', example_zh: '她习得了一项新技能。', topic_code: 'LEARNING', audio_url_uk: '', audio_url_us: '', emoji: '📚', pos: 'v.' },
  { id: 11, level_code: 'CET4', word: 'adapt',     ipa_uk: '/əˈdæpt/', ipa_us: '/əˈdæpt/', en_definition: 'To change to suit new conditions.', zh_definition: 'v. 适应；改编', example_en: 'Animals adapt to their environment.', example_zh: '动物适应环境。', topic_code: 'NATURE', audio_url_uk: '', audio_url_us: '', emoji: '🦎', pos: 'v.' },
  { id: 12, level_code: 'CET4', word: 'adequate',  ipa_uk: '/ˈæd.ɪ.kwət/', ipa_us: '/ˈæd.ɪ.kwɪt/', en_definition: 'Enough or satisfactory for a particular purpose.', zh_definition: 'adj. 足够的；适当的', example_en: 'The food supply is adequate.', example_zh: '食物供应是充足的。', topic_code: 'QUANTITY', audio_url_uk: '', audio_url_us: '', emoji: '✅', pos: 'adj.' },
  { id: 13, level_code: 'CET4', word: 'adjust',    ipa_uk: '/əˈdʒʌst/', ipa_us: '/əˈdʒʌst/', en_definition: 'To change slightly to achieve a better result.', zh_definition: 'v. 调整；适应', example_en: 'Please adjust the volume.', example_zh: '请调节音量。', topic_code: 'ACTION', audio_url_uk: '', audio_url_us: '', emoji: '🔧', pos: 'v.' },
  { id: 14, level_code: 'CET4', word: 'admire',    ipa_uk: '/ədˈmaɪər/', ipa_us: '/ədˈmaɪr/', en_definition: 'To regard with respect and warm approval.', zh_definition: 'v. 钦佩；欣赏', example_en: 'I admire her courage.', example_zh: '我钦佩她的勇气。', topic_code: 'EMOTION', audio_url_uk: '', audio_url_us: '', emoji: '🌟', pos: 'v.' },
  { id: 15, level_code: 'CET4', word: 'adopt',     ipa_uk: '/əˈdɒpt/', ipa_us: '/əˈdɑːpt/', en_definition: 'To take on or use a new idea, method, or way of behaving.', zh_definition: 'v. 采用；收养', example_en: 'They adopted a new strategy.', example_zh: '他们采用了新策略。', topic_code: 'DECISION', audio_url_uk: '', audio_url_us: '', emoji: '💡', pos: 'v.' },
  { id: 16, level_code: 'CET4', word: 'advance',   ipa_uk: '/ədˈvɑːns/', ipa_us: '/ədˈvæns/', en_definition: 'To move or develop forward.', zh_definition: 'v. 前进；推进 n. 进步', example_en: 'Technology continues to advance.', example_zh: '技术持续进步。', topic_code: 'PROGRESS', audio_url_uk: '', audio_url_us: '', emoji: '🚀', pos: 'v.' },
  { id: 17, level_code: 'CET4', word: 'advocate',  ipa_uk: '/ˈæd.və.kət/', ipa_us: '/ˈæd.və.keɪt/', en_definition: 'To publicly support a particular policy or way of doing things.', zh_definition: 'v. 提倡；主张 n. 倡导者', example_en: 'She advocates for equal rights.', example_zh: '她倡导平等权利。', topic_code: 'SOCIETY', audio_url_uk: '', audio_url_us: '', emoji: '📢', pos: 'v.' },
  { id: 18, level_code: 'CET4', word: 'affect',    ipa_uk: '/əˈfekt/', ipa_us: '/əˈfekt/', en_definition: 'To have an influence on someone or something.', zh_definition: 'v. 影响；感动', example_en: 'The weather affects our mood.', example_zh: '天气影响我们的心情。', topic_code: 'INFLUENCE', audio_url_uk: '', audio_url_us: '', emoji: '🌦️', pos: 'v.' },
  { id: 19, level_code: 'CET4', word: 'allocate',  ipa_uk: '/ˈæl.ə.keɪt/', ipa_us: '/ˈæl.ə.keɪt/', en_definition: 'To give time, money, or resources to a particular purpose.', zh_definition: 'v. 分配；拨给', example_en: 'They allocated funds for research.', example_zh: '他们为研究拨款。', topic_code: 'MANAGEMENT', audio_url_uk: '', audio_url_us: '', emoji: '💰', pos: 'v.' },
  { id: 20, level_code: 'CET4', word: 'analyze',   ipa_uk: '/ˈæn.ə.laɪz/', ipa_us: '/ˈæn.ə.laɪz/', en_definition: 'To examine something in detail.', zh_definition: 'v. 分析；解析', example_en: 'We need to analyze the data.', example_zh: '我们需要分析数据。', topic_code: 'ACADEMIC', audio_url_uk: '', audio_url_us: '', emoji: '🔬', pos: 'v.' },
]

export default [
  {
    url: '/api/v1/words/levels',
    method: 'get',
    response: () => ({
      code: 0, msg: 'ok',
      data: [
        { code: 'KET',     name_zh: 'KET 剑桥入门', name_en: 'KET',   target_word_count: 1500,  sort_order: 1 },
        { code: 'PET',     name_zh: 'PET 剑桥初级', name_en: 'PET',   target_word_count: 3500,  sort_order: 2 },
        { code: 'FCE',     name_zh: 'FCE 剑桥中级', name_en: 'FCE',   target_word_count: 5000,  sort_order: 3 },
        { code: 'CAE',     name_zh: 'CAE 剑桥高级', name_en: 'CAE',   target_word_count: 7500,  sort_order: 4 },
        { code: 'PRIMARY', name_zh: '小学',          name_en: 'Primary', target_word_count: 800, sort_order: 5 },
        { code: 'JUNIOR',  name_zh: '初中',          name_en: 'Junior',  target_word_count: 2000, sort_order: 6 },
        { code: 'SENIOR',  name_zh: '高中',          name_en: 'Senior',  target_word_count: 3500, sort_order: 7 },
        { code: 'CET4',    name_zh: '大学四级',      name_en: 'CET-4', target_word_count: 4500,  sort_order: 8 },
        { code: 'CET6',    name_zh: '大学六级',      name_en: 'CET-6', target_word_count: 6000,  sort_order: 9 },
        { code: 'TEM8',    name_zh: '专业八级',      name_en: 'TEM-8', target_word_count: 13000, sort_order: 10 },
      ],
    }),
  },
  {
    url: '/api/v1/words/topics',
    method: 'get',
    response: () => ({
      code: 0, msg: 'ok',
      data: [
        { code: 'ACTION', name_zh: '动作' }, { code: 'ACADEMIC', name_zh: '学术' },
        { code: 'SCIENCE', name_zh: '科学' }, { code: 'EMOTION', name_zh: '情感' },
        { code: 'NATURE', name_zh: '自然' }, { code: 'SOCIETY', name_zh: '社会' },
        { code: 'DIGITAL', name_zh: '数字' }, { code: 'LAW', name_zh: '法律' },
      ],
    }),
  },
  {
    url: '/api/v1/words/search',
    method: 'get',
    response: ({ query }: { query: Record<string, string> }) => {
      const q = (query.q || '').toLowerCase()
      const filtered = words.filter(w => w.word.includes(q) || w.zh_definition.includes(q))
      return { code: 0, msg: 'ok', data: { items: filtered, total: filtered.length, page: 1, page_size: 20 } }
    },
  },
  {
    url: '/api/v1/words/download',
    method: 'get',
    response: ({ query }: { query: Record<string, string> }) => ({
      code: 0, msg: 'ok',
      data: { level_code: query.level, version: '20260419', total: words.length, words },
    }),
  },
] as MockMethod[]
