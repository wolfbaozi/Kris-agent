import { request } from './request'
import type { SkillConfig, SkillFormData } from '../types/skill'

export type { SkillConfig, SkillFormData }

export const skillApi = {
  list: (): Promise<SkillConfig[]> => request('/skills'),

  create: (data: SkillFormData) =>
    request('/skills', { method: 'POST', body: JSON.stringify(data) }),

  update: (id: number, data: SkillFormData & { isGlobal?: number }) =>
    request('/skills/' + id, { method: 'PUT', body: JSON.stringify(data) }),

  remove: (id: number) =>
    request('/skills/' + id, { method: 'DELETE' }),

  toggle: (id: number) =>
    request('/skills/' + id + '/toggle', { method: 'PATCH' }),
}
