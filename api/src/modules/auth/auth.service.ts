import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwt: JwtService,
    private readonly config: ConfigService,
  ) {}

  async sendOtp(phone: string): Promise<{ message: string }> {
    const code = this.config.get('OTP_DEV_MODE') === 'true'
      ? '123456'
      : Math.floor(100000 + Math.random() * 900000).toString();

    const expiresAt = new Date(Date.now() + 60 * 1000);

    await this.prisma.otpCode.create({
      data: { phone, code, expiresAt },
    });

    return { message: 'OTP sent' };
  }

  async verifyOtp(phone: string, code: string): Promise<{
    accessToken: string;
    refreshToken: string;
    isNewUser: boolean;
  }> {
    const otp = await this.prisma.otpCode.findFirst({
      where: {
        phone,
        code,
        verified: false,
        expiresAt: { gte: new Date() },
      },
      orderBy: { createdAt: 'desc' },
    });

    if (!otp) {
      throw new UnauthorizedException('Invalid or expired OTP');
    }

    await this.prisma.otpCode.update({
      where: { id: otp.id },
      data: { verified: true },
    });

    const user = await this.prisma.user.findUnique({ where: { phone } });
    const isNewUser = !user;

    const tokenPayload = { phone, sub: user?.id ?? 'pending' };
    const accessToken = this.generateAccessToken(tokenPayload);
    const refreshToken = this.generateRefreshToken(tokenPayload);

    return { accessToken, refreshToken, isNewUser };
  }

  async register(phone: string, name: string, storeName: string): Promise<{
    accessToken: string;
    refreshToken: string;
    user: { id: string; phone: string; name: string };
    store: { id: string; name: string };
  }> {
    const existing = await this.prisma.user.findUnique({ where: { phone } });
    if (existing) {
      throw new ConflictException('User already exists');
    }

    const result = await this.prisma.$transaction(async (tx) => {
      const user = await tx.user.create({
        data: { phone, name },
      });

      const store = await tx.store.create({
        data: { name: storeName },
      });

      await tx.userStore.create({
        data: { userId: user.id, storeId: store.id, role: 'OWNER' },
      });

      return { user, store };
    });

    const tokenPayload = { phone, sub: result.user.id };
    const accessToken = this.generateAccessToken(tokenPayload);
    const refreshToken = this.generateRefreshToken(tokenPayload);

    return {
      accessToken,
      refreshToken,
      user: { id: result.user.id, phone: result.user.phone, name: result.user.name! },
      store: { id: result.store.id, name: result.store.name },
    };
  }

  async refresh(refreshToken: string): Promise<{
    accessToken: string;
    refreshToken: string;
  }> {
    try {
      const payload = this.jwt.verify(refreshToken, {
        secret: this.config.get('JWT_REFRESH_SECRET'),
      });

      const user = await this.prisma.user.findUnique({ where: { id: payload.sub } });
      if (!user) {
        throw new UnauthorizedException('User not found');
      }

      const tokenPayload = { phone: user.phone, sub: user.id };
      return {
        accessToken: this.generateAccessToken(tokenPayload),
        refreshToken: this.generateRefreshToken(tokenPayload),
      };
    } catch {
      throw new UnauthorizedException('Invalid refresh token');
    }
  }

  private generateAccessToken(payload: { phone: string; sub: string }): string {
    return this.jwt.sign(payload, {
      secret: this.config.get('JWT_SECRET'),
      expiresIn: '15m',
    });
  }

  private generateRefreshToken(payload: { phone: string; sub: string }): string {
    return this.jwt.sign(payload, {
      secret: this.config.get('JWT_REFRESH_SECRET'),
      expiresIn: '7d',
    });
  }
}
