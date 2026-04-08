import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { CreateProductDto } from './dto/create-product.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import { ProductQueryDto } from './dto/product-query.dto';

@Injectable()
export class ProductsService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(storeId: string, query: ProductQueryDto) {
    const limit = query.limit ?? 20;
    const where: any = { storeId, isDeleted: false };
    if (query.search) {
      where.OR = [
        { name: { contains: query.search, mode: 'insensitive' } },
        { barcode: { contains: query.search } },
      ];
    }
    const items = await this.prisma.product.findMany({
      where, include: { category: true }, orderBy: { createdAt: 'desc' },
      take: limit + 1,
      ...(query.cursor ? { cursor: { id: query.cursor }, skip: 1 } : {}),
    });
    const hasMore = items.length > limit;
    const products = hasMore ? items.slice(0, limit) : items;
    const nextCursor = hasMore ? products[products.length - 1].id : null;
    return { products, nextCursor };
  }

  async findOne(storeId: string, id: string) {
    const product = await this.prisma.product.findFirst({
      where: { id, storeId, isDeleted: false }, include: { category: true },
    });
    if (!product) throw new NotFoundException('Product not found');
    return product;
  }

  async create(storeId: string, dto: CreateProductDto) {
    return this.prisma.product.create({
      data: {
        name: dto.name, barcode: dto.barcode, sku: dto.sku, price: dto.price,
        costPrice: dto.costPrice ?? 0, quantity: dto.quantity ?? 0,
        unit: dto.unit ?? 'шт', categoryId: dto.categoryId, imageUrl: dto.imageUrl, storeId,
      },
      include: { category: true },
    });
  }

  async update(storeId: string, id: string, dto: UpdateProductDto) {
    await this.findOne(storeId, id);
    return this.prisma.product.update({ where: { id }, data: dto, include: { category: true } });
  }

  async softDelete(storeId: string, id: string) {
    await this.findOne(storeId, id);
    return this.prisma.product.update({ where: { id }, data: { isDeleted: true } });
  }

  async getCategories(storeId: string) {
    return this.prisma.category.findMany({ where: { storeId }, orderBy: { name: 'asc' } });
  }

  async createCategory(storeId: string, name: string) {
    return this.prisma.category.create({ data: { name, storeId } });
  }
}
