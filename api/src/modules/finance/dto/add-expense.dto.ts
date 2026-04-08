import { IsString, IsNumber, IsOptional, Min } from 'class-validator';

export class AddExpenseDto {
  @IsNumber() @Min(0.01)
  amount!: number;

  @IsOptional() @IsString()
  description?: string;

  @IsOptional() @IsString()
  categoryId?: string;
}
