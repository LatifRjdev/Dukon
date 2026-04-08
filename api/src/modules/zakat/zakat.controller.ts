import { Controller, Get, Post, Patch, Body, Param, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { ZakatService } from './zakat.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { IsNumber, IsOptional, Min } from 'class-validator';

class UpdateConfigDto {
  @IsOptional() @IsNumber() @Min(0)
  goldRatePerGram?: number;

  @IsOptional() @IsNumber() @Min(0)
  silverRatePerGram?: number;
}

@Controller('stores/:storeId/zakat')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true, transform: true }))
export class ZakatController {
  constructor(private readonly zakatService: ZakatService) {}

  @Get('calculate')
  async calculate(@Param('storeId') storeId: string) {
    return this.zakatService.calculate(storeId);
  }

  @Post('save')
  async save(@Param('storeId') storeId: string) {
    return this.zakatService.save(storeId);
  }

  @Get('history')
  async history(@Param('storeId') storeId: string) {
    return this.zakatService.getHistory(storeId);
  }

  @Get('config')
  async getConfig(@Param('storeId') storeId: string) {
    return this.zakatService.getConfig(storeId);
  }

  @Patch('config')
  async updateConfig(@Param('storeId') storeId: string, @Body() dto: UpdateConfigDto) {
    return this.zakatService.updateConfig(storeId, dto.goldRatePerGram, dto.silverRatePerGram);
  }
}
