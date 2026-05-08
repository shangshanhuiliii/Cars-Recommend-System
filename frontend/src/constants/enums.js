export const matchLevels = [
  'STRICT',
  'RELAX_BUDGET',
  'RELAX_BODY_TYPE',
  'RELAX_ENERGY_TYPE',
  'SIMILAR_RECOMMEND',
]

export const recommendStatuses = ['SUCCESS', 'FALLBACK', 'EMPTY']

export const bodyTypes = ['轿车', 'SUV', 'MPV', '跑车', '卡车']

export const carEnergyTypes = ['燃油', '纯电', '插混', '增程']

export const demandEnergyTypes = [...carEnergyTypes, '新能源']
