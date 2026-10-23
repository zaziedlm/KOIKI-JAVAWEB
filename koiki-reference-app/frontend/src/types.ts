export interface Option { id: string; code: string; name: string }
export interface Options { departments: Option[]; expenseCategories: Option[] }
export interface Item {
  expenseRequestId: string; department: Option; claimedAmount: string;
  status: string; version: string; updatedAt: string;
}
export interface Page { content: Item[]; totalElements: string; page: number; size: number }
export interface Detail extends Item {
  decisionReason: string | null;
  lines: { expenseLineId: string; expenseCategory: Option; usageDate: string;
    description: string; purpose: string; amount: string }[];
}
export interface Draft {
  departmentId: string; claimedAmount: string;
  lines: { expenseCategoryId: string; usageDate: string; description: string; purpose: string; amount: string }[];
}
export interface Problem { code?: string; violations?: { field: string; message: string }[] }
