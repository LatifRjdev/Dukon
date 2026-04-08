import { IsString, IsNumber, IsOptional, IsArray, ValidateNested, IsEnum, Min } from 'class-validator';
import { Type } from 'class-transformer';

export class CreateSaleItemDto {
  @IsString() productId!: string;
  @IsString() name!: string;
  @IsNumber() @Min(1) quantity!: number;
  @IsNumber() @Min(0) price!: number;
  @IsOptional() @IsNumber() @Min(0) discount?: number;
}

export class CreateSaleDto {
  @IsArray() @ValidateNested({ each: true }) @Type(() => CreateSaleItemDto)
  items!: CreateSaleItemDto[];
  @IsNumber() @Min(0) totalAmount!: number;
  @IsOptional() @IsNumber() @Min(0) discount?: number;
  @IsOptional() @IsEnum(['CASH', 'CARD', 'MIXED'] as const) paymentMethod?: 'CASH' | 'CARD' | 'MIXED';
  @IsOptional() @IsString() customerId?: string;
}
