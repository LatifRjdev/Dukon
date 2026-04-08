import { Controller, Get, Post, Patch, Body, Param, Query, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { CustomersService } from './customers.service';
import { CreateCustomerDto } from './dto/create-customer.dto';
import { UpdateCustomerDto } from './dto/update-customer.dto';
import { CustomerQueryDto } from './dto/customer-query.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('stores/:storeId/customers')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
export class CustomersController {
  constructor(private readonly customersService: CustomersService) {}

  @Get()
  async findAll(@Param('storeId') storeId: string, @Query() query: CustomerQueryDto) {
    return this.customersService.findAll(storeId, query);
  }

  @Get(':id')
  async findOne(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.customersService.findOne(storeId, id);
  }

  @Post()
  async create(@Param('storeId') storeId: string, @Body() dto: CreateCustomerDto) {
    return this.customersService.create(storeId, dto);
  }

  @Patch(':id')
  async update(@Param('storeId') storeId: string, @Param('id') id: string, @Body() dto: UpdateCustomerDto) {
    return this.customersService.update(storeId, id, dto);
  }

  @Get(':id/purchases')
  async getPurchases(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.customersService.getPurchases(storeId, id);
  }
}
