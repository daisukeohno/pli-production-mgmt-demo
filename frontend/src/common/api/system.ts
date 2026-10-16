import { api } from './client';

export interface SystemDate {
  /** GET_SYS_DATE（YYYY-MM-DD） */
  sysDate: string;
  /** 画面1行目の表示（YYYY/MM/DD） */
  screenDate: string;
}

export const fetchSystemDate = () => api.get<SystemDate>('/api/system/date');
