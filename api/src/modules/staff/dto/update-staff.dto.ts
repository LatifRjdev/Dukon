import { IsOptional, IsString, IsEnum, IsBoolean } from 'class-validator';

export class UpdateStaffDto {
  @IsOptional()
  @IsEnum(['OWNER', 'MANAGER', 'CASHIER'] as const)
  role?: 'OWNER' | 'MANAGER' | 'CASHIER';

  @IsOptional()
  @IsBoolean()
  isActive?: boolean;
}
