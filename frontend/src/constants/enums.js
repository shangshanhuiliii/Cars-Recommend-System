export const matchLevels = [
  'STRICT',
  'RELAX_BUDGET',
  'RELAX_BODY_TYPE',
  'RELAX_ENERGY_TYPE',
  'SIMILAR_RECOMMEND',
]

export const recommendStatuses = ['SUCCESS', 'FALLBACK', 'EMPTY']

export const bodyTypes = ['SUV', '轿车', 'MPV']

export const carEnergyTypes = ['燃油', '纯电', '插混', '增程']

export const demandEnergyTypes = [...carEnergyTypes, '新能源']
