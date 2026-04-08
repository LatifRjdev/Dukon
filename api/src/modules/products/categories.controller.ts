import { Controller, Get, Post, Body, Param, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { ProductsService } from './products.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { IsString, MinLength } from 'class-validator';

class CreateCategoryDto {
  @IsString() @MinLength(1)
  name!: string;
}

@Controller('stores/:storeId/categories')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true }))
export class CategoriesController {
  constructor(private readonly productsService: ProductsService) {}

  @Get()
  async findAll(@Param('storeId') storeId: string) {
    return this.productsService.getCategories(storeId);
  }

  @Post()
  async create(@Param('storeId') storeId: string, @Body() dto: CreateCategoryDto) {
    return this.productsService.createCategory(storeId, dto.name);
  }
}
