import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreateSaleDto } from './dto/create-sale.dto';
import { SaleQueryDto } from './dto/sale-query.dto';

@Injectable()
export class SalesService {
  constructor(private readonly prisma: PrismaService) {}

  async create(storeId: string, dto: CreateSaleDto) {
    return this.prisma.$transaction(async (tx) => {
      for (const item of dto.items) {
        const product = await tx.product.findFirst({
          where: { id: item.productId, storeId, isDeleted: false },
        });
        if (!product) throw new NotFoundException(`Product ${item.productId} not found`);
        if (product.quantity < item.quantity) {
          throw new BadRequestException(`Insufficient stock for ${product.name}`);
        }
        await tx.product.update({
          where: { id: item.productId },
          data: { quantity: { decrement: item.quantity } },
        });
      }

      return tx.sale.create({
        data: {
          totalAmount: dto.totalAmount,
          discount: dto.discount ?? 0,
          paymentMethod: dto.paymentMethod ?? 'CASH',
          customerId: dto.customerId,
          storeId,
          items: {
            create: dto.items.map((item) => ({
              productId: item.productId,
              name: item.name,
              quantity: item.quantity,
              price: item.price,
              discount: item.discount ?? 0,
            })),
          },
        },
        include: { items: true },
      });
    });
  }

  async findAll(storeId: string, query: SaleQueryDto) {
    const limit = query.limit ?? 20;
    const where: any = { storeId };
    if (query.dateFrom || query.dateTo) {
      where.createdAt = {};
      if (query.dateFrom) where.createdAt.gte = new Date(query.dateFrom);
      if (query.dateTo) where.createdAt.lte = new Date(query.dateTo);
    }
    const items = await this.prisma.sale.findMany({
      where, include: { items: true }, orderBy: { createdAt: 'desc' },
      take: limit + 1,
      ...(query.cursor ? { cursor: { id: query.cursor }, skip: 1 } : {}),
    });
    const hasMore = items.length > limit;
    const sales = hasMore ? items.slice(0, limit) : items;
    const nextCursor = hasMore ? sales[sales.length - 1].id : null;
    return { sales, nextCursor };
  }

  async findOne(storeId: string, id: string) {
    const sale = await this.prisma.sale.findFirst({
      where: { id, storeId }, include: { items: true },
    });
    if (!sale) throw new NotFoundException('Sale not found');
    return sale;
  }

  async refund(storeId: string, id: string) {
    const sale = await this.findOne(storeId, id);
    if (sale.isRefunded) throw new BadRequestException('Sale already refunded');
    return this.prisma.$transaction(async (tx) => {
      for (const item of sale.items) {
        await tx.product.update({
          where: { id: item.productId },
          data: { quantity: { increment: item.quantity } },
        });
      }
      return tx.sale.update({
        where: { id }, data: { isRefunded: true }, include: { items: true },
      });
    });
  }
}
