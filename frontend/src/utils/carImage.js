export const DEFAULT_CAR_IMAGE = '/images/cars/default-car.svg'

export function carImageSrc(imageUrl) {
  if (typeof imageUrl === 'string' && imageUrl.trim()) {
    return imageUrl.trim()
  }
  return DEFAULT_CAR_IMAGE
}

export function fallbackCarImage(event) {
  const target = event?.target
  if (!target || target.dataset.fallbackApplied === 'true') {
    return
  }
  target.dataset.fallbackApplied = 'true'
  target.src = DEFAULT_CAR_IMAGE
}
