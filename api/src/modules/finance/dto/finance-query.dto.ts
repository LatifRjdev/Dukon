import { IsOptional, IsString, IsEnum } from 'class-validator';

export class FinanceQueryDto {
  @IsOptional() @IsEnum(['INCOME', 'EXPENSE'] as const)
  type?: 'INCOME' | 'EXPENSE';

  @IsOptional() @IsString()
  dateFrom?: string;

  @IsOptional() @IsString()
  dateTo?: string;

  @IsOptional() @IsString()
  period?: 'day' | 'week' | 'month';
}
