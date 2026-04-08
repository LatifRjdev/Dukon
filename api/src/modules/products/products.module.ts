import { Module } from '@nestjs/common';
import { ProductsController } from './products.controller';
import { CategoriesController } from './categories.controller';
import { ProductsService } from './products.service';
import { AuthModule } from '../auth/auth.module';

@Module({
  imports: [AuthModule],
  controllers: [ProductsController, CategoriesController],
  providers: [ProductsService],
})
export class ProductsModule {}
