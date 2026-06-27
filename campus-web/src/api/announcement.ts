import request from './request'

export interface Announcement {
  id: number
  title: string
  summary: string
  content: string
  coverImage: string
  category: string
  priority: number
  isCarousel: number
  carouselSort: number
  status: string
  publisherId: number
  publishedAt: string
  createdAt: string
}

export interface AnnouncementAttachment {
  id: number
  announcementId: number
  fileName: string
  fileSize: number
  fileUrl: string
}

export function getAnnouncements(category?: string, page = 1, size = 10) {
  const params: any = { page, size }
  if (category) params.category = category
  return request.get<any, { data: { records: Announcement[]; total: number; page: number; size: number } }>(
    '/announcements',
    { params }
  )
}

export function getCarousel() {
  return request.get<any, { data: Announcement[] }>('/announcements/carousel')
}

export function getAnnouncementDetail(id: number) {
  return request.get<any, { data: Announcement }>(`/announcements/${id}`)
}

export function getAttachments(id: number) {
  return request.get<any, { data: AnnouncementAttachment[] }>(`/announcements/${id}/attachments`)
}

export function getAdminAnnouncements(page = 1, size = 10) {
  return request.get<any, { data: { records: Announcement[]; total: number; page: number; size: number } }>(
    '/admin/announcements',
    { params: { page, size } }
  )
}

export function createAnnouncement(data: Partial<Announcement>) {
  return request.post('/admin/announcements', data)
}

export function updateAnnouncement(id: number, data: Partial<Announcement>) {
  return request.put(`/admin/announcements/${id}`, data)
}

export function deleteAnnouncement(id: number) {
  return request.delete(`/admin/announcements/${id}`)
}

export function toggleCarousel(id: number, isCarousel: number, sort = 0) {
  return request.post(`/admin/announcements/${id}/carousel`, null, {
    params: { isCarousel, sort },
  })
}

export function uploadAttachment(id: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/admin/announcements/${id}/attachment`, formData)
}

export function deleteAttachment(id: number, attachmentId: number) {
  return request.delete(`/admin/announcements/${id}/attachment/${attachmentId}`)
}

export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, { data: string }>('/upload/image', formData)
}
