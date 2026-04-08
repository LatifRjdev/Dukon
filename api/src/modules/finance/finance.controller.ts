import { Controller, Get, Post, Body, Param, Query, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { FinanceService } from './finance.service';
import { AddExpenseDto } from './dto/add-expense.dto';
import { FinanceQueryDto } from './dto/finance-query.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { IsString, MinLength } from 'class-validator';

class CreateCategoryDto {
  @IsString() @MinLength(1)
  name!: string;
}

@Controller('stores/:storeId/finance')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
export class FinanceController {
  constructor(private readonly financeService: FinanceService) {}

  @Get('summary')
  async getSummary(@Param('storeId') storeId: string, @Query() query: FinanceQueryDto) {
    return this.financeService.getSummary(storeId, query);
  }

  @Get('transactions')
  async getTransactions(@Param('storeId') storeId: string, @Query() query: FinanceQueryDto) {
    return this.financeService.getTransactions(storeId, query);
  }

  @Post('expenses')
  async addExpense(@Param('storeId') storeId: string, @Body() dto: AddExpenseDto) {
    return this.financeService.addExpense(storeId, dto);
  }

  @Get('reports')
  async getReport(@Param('storeId') storeId: string, @Query() query: FinanceQueryDto) {
    return this.financeService.getReport(storeId, query);
  }

  @Get('categories')
  async getCategories(@Param('storeId') storeId: string) {
    return this.financeService.getExpenseCategories(storeId);
  }

  @Post('categories')
  async createCategory(@Param('storeId') storeId: string, @Body() dto: CreateCategoryDto) {
    return this.financeService.createExpenseCategory(storeId, dto.name);
  }
}
