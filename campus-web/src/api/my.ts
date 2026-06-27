import request from './request'

export interface MyItemVO {
  id: number
  type: 'course' | 'square_post' | 'market'
  targetId: number
  title: string
  description: string
  coverImage: string
  likeCount: number
  createdAt: string
}

export function getMyLikes(page = 1, size = 12) {
  return request.get<any, { code: number; data: MyItemVO[] }>(
    '/my/likes', { params: { page, size } }
  )
}

export function getMyFavorites(page = 1, size = 12) {
  return request.get<any, { code: number; data: MyItemVO[] }>(
    '/my/favorites', { params: { page, size } }
  )
}
