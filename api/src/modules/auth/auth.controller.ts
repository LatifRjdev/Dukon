import { Controller, Post, Body, UsePipes, ValidationPipe } from '@nestjs/common';
import { AuthService } from './auth.service';
import { SendOtpDto } from './dto/send-otp.dto';
import { VerifyOtpDto } from './dto/verify-otp.dto';
import { RegisterDto } from './dto/register.dto';
import { RefreshDto } from './dto/refresh.dto';

@Controller('auth')
@UsePipes(new ValidationPipe({ whitelist: true }))
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('send-otp')
  async sendOtp(@Body() dto: SendOtpDto) {
    return this.authService.sendOtp(dto.phone);
  }

  @Post('verify-otp')
  async verifyOtp(@Body() dto: VerifyOtpDto) {
    return this.authService.verifyOtp(dto.phone, dto.code);
  }

  @Post('register')
  async register(@Body() dto: RegisterDto) {
    return this.authService.register(dto.phone, dto.name, dto.storeName);
  }

  @Post('refresh')
  async refresh(@Body() dto: RefreshDto) {
    return this.authService.refresh(dto.refreshToken);
  }
}
