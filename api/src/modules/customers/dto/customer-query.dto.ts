import { IsOptional, IsString, IsNumber, Min } from 'class-validator';
import { Type } from 'class-transformer';

export class CustomerQueryDto {
  @IsOptional() @IsString()
  search?: string;

  @IsOptional() @IsString()
  cursor?: string;

  @IsOptional() @Type(() => Number) @IsNumber() @Min(1)
  limit?: number = 20;
}
