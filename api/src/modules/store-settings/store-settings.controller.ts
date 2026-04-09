import { Controller, Get, Patch, Body, Param, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { StoreSettingsService } from './store-settings.service';
import { UpdateStoreDto } from './dto/update-store.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('stores/:storeId')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true }))
export class StoreSettingsController {
  constructor(private readonly storeSettingsService: StoreSettingsService) {}

  @Get()
  async getStore(@Param('storeId') storeId: string) {
    return this.storeSettingsService.getStore(storeId);
  }

  @Patch()
  async updateStore(@Param('storeId') storeId: string, @Body() dto: UpdateStoreDto) {
    return this.storeSettingsService.updateStore(storeId, dto);
  }
}
