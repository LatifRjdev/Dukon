import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreateCustomerDto } from './dto/create-customer.dto';
import { UpdateCustomerDto } from './dto/update-customer.dto';
import { CustomerQueryDto } from './dto/customer-query.dto';

@Injectable()
export class CustomersService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(storeId: string, query: CustomerQueryDto) {
    const limit = query.limit ?? 20;
    const where: any = { storeId };
    if (query.search) {
      where.OR = [
        { name: { contains: query.search, mode: 'insensitive' } },
        { phone: { contains: query.search } },
      ];
    }
    const items = await this.prisma.customer.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      take: limit + 1,
      ...(query.cursor ? { cursor: { id: query.cursor }, skip: 1 } : {}),
    });
    const hasMore = items.length > limit;
    const customers = hasMore ? items.slice(0, limit) : items;
    const nextCursor = hasMore ? customers[customers.length - 1].id : null;
    return { customers, nextCursor };
  }

  async findOne(storeId: string, id: string) {
    const customer = await this.prisma.customer.findFirst({
      where: { id, storeId },
    });
    if (!customer) throw new NotFoundException('Customer not found');
    return customer;
  }

  async create(storeId: string, dto: CreateCustomerDto) {
    return this.prisma.customer.create({
      data: {
        name: dto.name,
        phone: dto.phone,
        email: dto.email,
        notes: dto.notes,
        storeId,
      },
    });
  }

  async update(storeId: string, id: string, dto: UpdateCustomerDto) {
    await this.findOne(storeId, id);
    return this.prisma.customer.update({ where: { id }, data: dto });
  }

  async getPurchases(storeId: string, customerId: string) {
    await this.findOne(storeId, customerId);
    return this.prisma.sale.findMany({
      where: { storeId, customerId },
      include: { items: true },
      orderBy: { createdAt: 'desc' },
    });
  }

  async updateStatsAfterSale(customerId: string, saleAmount: number) {
    return this.prisma.customer.update({
      where: { id: customerId },
      data: {
        totalSpent: { increment: saleAmount },
        visitCount: { increment: 1 },
      },
    });
  }
}
