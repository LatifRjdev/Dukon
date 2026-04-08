import { Controller, Get, Post, Patch, Delete, Body, Param, UseGuards, UsePipes, ValidationPipe } from '@nestjs/common';
import { StaffService } from './staff.service';
import { AddStaffDto } from './dto/add-staff.dto';
import { UpdateStaffDto } from './dto/update-staff.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('stores/:storeId/staff')
@UseGuards(JwtAuthGuard)
@UsePipes(new ValidationPipe({ whitelist: true }))
export class StaffController {
  constructor(private readonly staffService: StaffService) {}

  @Get()
  async findAll(@Param('storeId') storeId: string) {
    return this.staffService.findAll(storeId);
  }

  @Post()
  async add(@Param('storeId') storeId: string, @Body() dto: AddStaffDto) {
    return this.staffService.addStaff(storeId, dto);
  }

  @Patch(':id')
  async update(@Param('storeId') storeId: string, @Param('id') id: string, @Body() dto: UpdateStaffDto) {
    return this.staffService.updateStaff(storeId, id, dto);
  }

  @Delete(':id')
  async deactivate(@Param('storeId') storeId: string, @Param('id') id: string) {
    return this.staffService.deactivate(storeId, id);
  }
}
