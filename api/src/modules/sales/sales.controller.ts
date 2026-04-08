import { Controller, Get, Post, Body, Param, Query, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { SalesService } from './sales.service';
import { CreateSaleDto } from './dto/create-sale.dto';
import { SaleQueryDto } from './dto/sale-query.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('stores/:storeId/sales')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
export class SalesController {
  constructor(private readonly salesService: SalesService) {}

  @Post()
  async create(@Param('storeId') storeId: string, @Body() dto: CreateSaleDto) {
    return this.salesService.create(storeId, dto);
  }

  @Get()
  async findAll(@Param('storeId') storeId: string, @Query() query: SaleQueryDto) {
    return this.salesService.findAll(storeId, query);
  }

  @Get(':id')
  async findOne(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.salesService.findOne(storeId, id);
  }

  @Post(':id/refund')
  async refund(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.salesService.refund(storeId, id);
  }
}
