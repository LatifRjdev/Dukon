import { Controller, Get, Post, Patch, Delete, Body, Param, Query, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { ProductsService } from './products.service';
import { CreateProductDto } from './dto/create-product.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import { ProductQueryDto } from './dto/product-query.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('stores/:storeId/products')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
export class ProductsController {
  constructor(private readonly productsService: ProductsService) {}

  @Get()
  async findAll(@Param('storeId') storeId: string, @Query() query: ProductQueryDto) {
    return this.productsService.findAll(storeId, query);
  }

  @Get(':id')
  async findOne(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.productsService.findOne(storeId, id);
  }

  @Post()
  async create(@Param('storeId') storeId: string, @Body() dto: CreateProductDto) {
    return this.productsService.create(storeId, dto);
  }

  @Patch(':id')
  async update(@Param('storeId') storeId: string, @Param('id') id: string, @Body() dto: UpdateProductDto) {
    return this.productsService.update(storeId, id, dto);
  }

  @Delete(':id')
  async remove(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.productsService.softDelete(storeId, id);
  }
}
