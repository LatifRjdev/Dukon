import { IsString, Matches, Length } from 'class-validator';

export class VerifyOtpDto {
  @IsString()
  @Matches(/^\+992\d{9}$/, { message: 'Phone must be a valid Tajik number (+992XXXXXXXXX)' })
  phone!: string;

  @IsString()
  @Length(6, 6, { message: 'Code must be exactly 6 digits' })
  code!: string;
}
