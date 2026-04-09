import { Module } from '@nestjs/common';
import { StoreSettingsController } from './store-settings.controller';
import { StoreSettingsService } from './store-settings.service';
import { AuthModule } from '../auth/auth.module';

@Module({
  imports: [AuthModule],
  controllers: [StoreSettingsController],
  providers: [StoreSettingsService],
})
export class StoreSettingsModule {}
