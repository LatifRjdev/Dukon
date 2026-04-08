import { IsString, IsEnum, Matches } from 'class-validator';

export class AddStaffDto {
  @IsString()
  @Matches(/^\+992\d{9}$/, { message: 'Phone must be a valid Tajik number (+992XXXXXXXXX)' })
  phone!: string;

  @IsString()
  name!: string;

  @IsEnum(['OWNER', 'MANAGER', 'CASHIER'] as const)
  role!: 'OWNER' | 'MANAGER' | 'CASHIER';
}
