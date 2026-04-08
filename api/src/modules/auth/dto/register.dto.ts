import { IsString, Matches, MinLength } from 'class-validator';

export class RegisterDto {
  @IsString()
  @Matches(/^\+992\d{9}$/, { message: 'Phone must be a valid Tajik number (+992XXXXXXXXX)' })
  phone!: string;

  @IsString()
  @MinLength(2, { message: 'Name must be at least 2 characters' })
  name!: string;

  @IsString()
  @MinLength(2, { message: 'Store name must be at least 2 characters' })
  storeName!: string;
}
