import { IsString, IsNumber, IsOptional, Min } from 'class-validator';

export class UpdateProductDto {
  @IsOptional() @IsString() name?: string;
  @IsOptional() @IsString() barcode?: string;
  @IsOptional() @IsString() sku?: string;
  @IsOptional() @IsNumber() @Min(0) price?: number;
  @IsOptional() @IsNumber() @Min(0) costPrice?: number;
  @IsOptional() @IsNumber() @Min(0) quantity?: number;
  @IsOptional() @IsString() unit?: string;
  @IsOptional() @IsString() categoryId?: string;
  @IsOptional() @IsString() imageUrl?: string;
}
