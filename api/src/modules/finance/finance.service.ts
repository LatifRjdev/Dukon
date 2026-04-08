import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { AddExpenseDto } from './dto/add-expense.dto';
import { FinanceQueryDto } from './dto/finance-query.dto';

@Injectable()
export class FinanceService {
  constructor(private readonly prisma: PrismaService) {}

  async getSummary(storeId: string, query: FinanceQueryDto) {
    const dateRange = this.getDateRange(query.period ?? 'day');
    const where = { storeId, createdAt: { gte: dateRange.from, lte: dateRange.to } };

    const [income, expenses] = await Promise.all([
      this.prisma.transaction.aggregate({ where: { ...where, type: 'INCOME' }, _sum: { amount: true } }),
      this.prisma.transaction.aggregate({ where: { ...where, type: 'EXPENSE' }, _sum: { amount: true } }),
    ]);

    const revenue = income._sum.amount ?? 0;
    const expense = expenses._sum.amount ?? 0;

    return {
      revenue,
      expenses: expense,
      profit: revenue - expense,
      period: query.period ?? 'day',
      dateFrom: dateRange.from.toISOString(),
      dateTo: dateRange.to.toISOString(),
    };
  }

  async getTransactions(storeId: string, query: FinanceQueryDto) {
    const where: any = { storeId };
    if (query.type) where.type = query.type;
    if (query.dateFrom || query.dateTo) {
      where.createdAt = {};
      if (query.dateFrom) where.createdAt.gte = new Date(query.dateFrom);
      if (query.dateTo) where.createdAt.lte = new Date(query.dateTo);
    }

    return this.prisma.transaction.findMany({
      where,
      include: { category: true },
      orderBy: { createdAt: 'desc' },
      take: 50,
    });
  }

  async addExpense(storeId: string, dto: AddExpenseDto) {
    return this.prisma.transaction.create({
      data: {
        type: 'EXPENSE',
        amount: dto.amount,
        description: dto.description,
        categoryId: dto.categoryId,
        storeId,
      },
      include: { category: true },
    });
  }

  async getReport(storeId: string, query: FinanceQueryDto) {
    const dateRange = this.getDateRange(query.period ?? 'week');
    const transactions = await this.prisma.transaction.findMany({
      where: {
        storeId,
        type: 'INCOME',
        createdAt: { gte: dateRange.from, lte: dateRange.to },
      },
      orderBy: { createdAt: 'asc' },
    });

    const dailyMap = new Map<string, number>();
    for (const tx of transactions) {
      const day = tx.createdAt.toISOString().split('T')[0];
      dailyMap.set(day, (dailyMap.get(day) ?? 0) + tx.amount);
    }

    return Array.from(dailyMap.entries()).map(([date, amount]) => ({ date, amount }));
  }

  async getExpenseCategories(storeId: string) {
    return this.prisma.expenseCategory.findMany({ where: { storeId }, orderBy: { name: 'asc' } });
  }

  async createExpenseCategory(storeId: string, name: string) {
    return this.prisma.expenseCategory.create({ data: { name, storeId } });
  }

  private getDateRange(period: string): { from: Date; to: Date } {
    const now = new Date();
    const to = new Date(now);
    to.setHours(23, 59, 59, 999);

    const from = new Date(now);
    from.setHours(0, 0, 0, 0);

    switch (period) {
      case 'week':
        from.setDate(from.getDate() - 6);
        break;
      case 'month':
        from.setDate(from.getDate() - 29);
        break;
      case 'day':
      default:
        break;
    }
    return { from, to };
  }
}
