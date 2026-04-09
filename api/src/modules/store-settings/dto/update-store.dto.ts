import { IsString, IsOptional, MinLength } from 'class-validator';

export class UpdateStoreDto {
  @IsOptional() @IsString() @MinLength(1) name?: string;
  @IsOptional() @IsString() address?: string;
  @IsOptional() @IsString() phone?: string;
  @IsOptional() @IsString() currency?: string;
  @IsOptional() @IsString() logoUrl?: string;
  @IsOptional() @IsString() receiptHeader?: string;
  @IsOptional() @IsString() receiptFooter?: string;
}
