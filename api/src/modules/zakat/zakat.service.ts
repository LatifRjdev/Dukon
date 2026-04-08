import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class ZakatService {
  constructor(private readonly prisma: PrismaService) {}

  async calculate(storeId: string) {
    const config = await this.getOrCreateConfig(storeId);
    const nisabThreshold = 85 * config.goldRatePerGram;

    // Inventory value = sum of (costPrice * quantity) per product
    const products = await this.prisma.product.findMany({
      where: { storeId, isDeleted: false },
      select: { costPrice: true, quantity: true },
    });
    const inventoryValue = products.reduce((sum, p) => sum + p.costPrice * p.quantity, 0);

    // Cash balance = total INCOME - total EXPENSE
    const [incomeAgg, expenseAgg] = await Promise.all([
      this.prisma.transaction.aggregate({ where: { storeId, type: 'INCOME' }, _sum: { amount: true } }),
      this.prisma.transaction.aggregate({ where: { storeId, type: 'EXPENSE' }, _sum: { amount: true } }),
    ]);
    const cashBalance = (incomeAgg._sum.amount ?? 0) - (expenseAgg._sum.amount ?? 0);

    const totalAssets = inventoryValue + Math.max(cashBalance, 0);
    const zakatableAmount = Math.max(totalAssets - nisabThreshold, 0);
    const zakatDue = totalAssets >= nisabThreshold ? zakatableAmount * 0.025 : 0;

    return {
      inventoryValue: Math.round(inventoryValue * 100) / 100,
      cashBalance: Math.round(cashBalance * 100) / 100,
      receivables: 0,
      liabilities: 0,
      nisabThreshold: Math.round(nisabThreshold * 100) / 100,
      zakatableAmount: Math.round(zakatableAmount * 100) / 100,
      zakatDue: Math.round(zakatDue * 100) / 100,
      goldRate: config.goldRatePerGram,
      storeId,
    };
  }

  async save(storeId: string) {
    const calc = await this.calculate(storeId);
    return this.prisma.zakatCalculation.create({
      data: {
        inventoryValue: calc.inventoryValue,
        cashBalance: calc.cashBalance,
        receivables: calc.receivables,
        liabilities: calc.liabilities,
        nisabThreshold: calc.nisabThreshold,
        zakatableAmount: calc.zakatableAmount,
        zakatDue: calc.zakatDue,
        goldRate: calc.goldRate,
        storeId,
      },
    });
  }

  async getHistory(storeId: string) {
    return this.prisma.zakatCalculation.findMany({
      where: { storeId },
      orderBy: { calculatedAt: 'desc' },
      take: 20,
    });
  }

  async getConfig(storeId: string) {
    return this.getOrCreateConfig(storeId);
  }

  async updateConfig(storeId: string, goldRate?: number, silverRate?: number) {
    const config = await this.getOrCreateConfig(storeId);
    return this.prisma.zakatConfig.update({
      where: { id: config.id },
      data: {
        ...(goldRate !== undefined ? { goldRatePerGram: goldRate } : {}),
        ...(silverRate !== undefined ? { silverRatePerGram: silverRate } : {}),
      },
    });
  }

  private async getOrCreateConfig(storeId: string) {
    let config = await this.prisma.zakatConfig.findUnique({ where: { storeId } });
    if (!config) {
      config = await this.prisma.zakatConfig.create({
        data: { storeId, goldRatePerGram: 750, silverRatePerGram: 10 },
      });
    }
    return config;
  }
}
