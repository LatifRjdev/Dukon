import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { AddStaffDto } from './dto/add-staff.dto';
import { UpdateStaffDto } from './dto/update-staff.dto';

@Injectable()
export class StaffService {
  constructor(private readonly prisma: PrismaService) {}

  async findAll(storeId: string) {
    const userStores = await this.prisma.userStore.findMany({
      where: { storeId },
      include: { user: true },
    });
    return userStores.map((us) => ({
      id: us.id,
      userId: us.userId,
      name: us.user.name,
      phone: us.user.phone,
      role: us.role,
      storeId: us.storeId,
    }));
  }

  async addStaff(storeId: string, dto: AddStaffDto) {
    let user = await this.prisma.user.findUnique({ where: { phone: dto.phone } });

    if (!user) {
      user = await this.prisma.user.create({
        data: { phone: dto.phone, name: dto.name },
      });
    }

    const existing = await this.prisma.userStore.findUnique({
      where: { userId_storeId: { userId: user.id, storeId } },
    });
    if (existing) {
      throw new ConflictException('User is already a staff member of this store');
    }

    const userStore = await this.prisma.userStore.create({
      data: { userId: user.id, storeId, role: dto.role },
    });

    return {
      id: userStore.id,
      userId: user.id,
      name: user.name,
      phone: user.phone,
      role: userStore.role,
      storeId,
    };
  }

  async updateStaff(storeId: string, id: string, dto: UpdateStaffDto) {
    const userStore = await this.prisma.userStore.findFirst({ where: { id, storeId } });
    if (!userStore) throw new NotFoundException('Staff member not found');

    const updated = await this.prisma.userStore.update({
      where: { id },
      data: { ...(dto.role ? { role: dto.role } : {}) },
      include: { user: true },
    });

    return {
      id: updated.id,
      userId: updated.userId,
      name: updated.user.name,
      phone: updated.user.phone,
      role: updated.role,
      storeId: updated.storeId,
    };
  }

  async deactivate(storeId: string, id: string) {
    const userStore = await this.prisma.userStore.findFirst({ where: { id, storeId } });
    if (!userStore) throw new NotFoundException('Staff member not found');

    await this.prisma.userStore.delete({ where: { id } });
    return { message: 'Staff member deactivated' };
  }
}
