import { IsString, Matches } from 'class-validator';

export class SendOtpDto {
  @IsString()
  @Matches(/^\+992\d{9}$/, { message: 'Phone must be a valid Tajik number (+992XXXXXXXXX)' })
  phone!: string;
}
