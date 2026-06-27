import request from './request'

export interface MarketItemVO {
  id: number
  userId: number
  userNickname: string
  userAvatar: string
  title: string
  description: string
  price: number
  originalPrice: number | null
  category: string
  condition: string
  images: string
  imageUrls: string[]
  status: string
  viewCount: number
  likeCount: number
  liked: boolean
  createdAt: string
  updatedAt: string
}

export interface MarketCommentVO {
  id: number
  itemId: number
  userId: number
  userNickname: string
  userAvatar: string
  parentId: number | null
  replyToUserId: number | null
  replyToUserNickname: string | null
  content: string
  likeCount: number
  liked: boolean
  createdAt: string
  replies: MarketCommentVO[] | null
}

export interface OfferVO {
  id: number
  itemId: number
  itemTitle: string
  buyerId: number
  buyerName: string
  buyerAvatar: string | null
  sellerId: number
  sellerName: string
  sellerAvatar: string | null
  price: number
  message: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface PublishItemRequest {
  title: string
  description: string
  price: number
  originalPrice?: number
  category: string
  condition: string
  images: string[]
}

export function getMarketItems(params: {
  category?: string; condition?: string; keyword?: string
  minPrice?: number; maxPrice?: number; sort?: string
  page?: number; size?: number
}) {
  return request.get<any, { code: number; data: { total: number; records: MarketItemVO[] } }>(
    '/market', { params }
  )
}

export function getMarketItemDetail(id: number) {
  return request.get<any, { code: number; data: MarketItemVO }>(`/market/${id}`)
}

export function publishMarketItem(data: PublishItemRequest) {
  return request.post('/market', data)
}

export function updateMarketItem(id: number, data: PublishItemRequest) {
  return request.put(`/market/${id}`, data)
}

export function markItemSold(id: number) {
  return request.post(`/market/${id}/sold`)
}

export function deleteMarketItem(id: number) {
  return request.delete(`/market/${id}`)
}

export function getMyMarketItems(page = 1, size = 20) {
  return request.get<any, { code: number; data: { total: number; records: MarketItemVO[] } }>(
    '/market/my', { params: { page, size } }
  )
}

export function likeMarketItem(id: number) {
  return request.post<any, { code: number; data: { liked: boolean } }>(`/market/${id}/like`)
}

export function getMarketComments(itemId: number) {
  return request.get<any, { code: number; data: MarketCommentVO[] }>(`/market/${itemId}/comments`)
}

export function addMarketComment(itemId: number, content: string, parentId?: number, replyToUserId?: number) {
  return request.post<any, { code: number; data: { success: boolean; comment: MarketCommentVO } }>(
    `/market/${itemId}/comments`,
    { content, parentId, replyToUserId }
  )
}

export function likeMarketComment(commentId: number) {
  return request.post<any, { code: number; data: { liked: boolean } }>(`/market/comments/${commentId}/like`)
}

export function deleteMarketComment(commentId: number) {
  return request.delete(`/market/comments/${commentId}`)
}

export function makeOffer(itemId: number, price: number, message?: string) {
  return request.post(`/market/${itemId}/offer`, { price, message })
}

export function getReceivedOffers(page = 1, size = 20) {
  return request.get<any, { code: number; data: { total: number; records: OfferVO[] } }>(
    '/market/offers/received', { params: { page, size } }
  )
}

export function getSentOffers(page = 1, size = 20) {
  return request.get<any, { code: number; data: { total: number; records: OfferVO[] } }>(
    '/market/offers/sent', { params: { page, size } }
  )
}

export function acceptOffer(offerId: number) {
  return request.put(`/market/offers/${offerId}/accept`)
}

export function rejectOffer(offerId: number) {
  return request.put(`/market/offers/${offerId}/reject`)
}
