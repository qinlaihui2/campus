import request from './request'

export interface LostFoundPost {
  id: number
  type: string
  title: string
  description: string
  category: string
  location: string
  contactWay: string
  imageUrls: string
  status: string
  publisherId: number
  publisherName: string
  publisherAvatar: string | null
  publishedAt: string
  createdAt: string
}

export function getPosts(type?: string, category?: string, keyword?: string, page = 1, size = 12) {
  return request.get<any, { data: { records: LostFoundPost[]; total: number } }>(
    '/lost-found', { params: { type, category, keyword, page, size } }
  )
}

export function getPostDetail(id: number) {
  return request.get<any, { data: LostFoundPost }>(`/lost-found/${id}`)
}

export function publishPost(data: Partial<LostFoundPost>) {
  return request.post('/lost-found', data)
}

export function updatePost(id: number, data: Partial<LostFoundPost>) {
  return request.put(`/lost-found/${id}`, data)
}

export function resolvePost(id: number) {
  return request.post(`/lost-found/${id}/resolve`)
}

export function deletePost(id: number) {
  return request.delete(`/admin/lost-found/${id}`)
}
