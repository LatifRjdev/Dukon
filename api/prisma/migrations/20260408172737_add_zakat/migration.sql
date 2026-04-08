-- CreateTable
CREATE TABLE "zakat_configs" (
    "id" TEXT NOT NULL,
    "store_id" TEXT NOT NULL,
    "gold_rate_per_gram" DOUBLE PRECISION NOT NULL DEFAULT 750,
    "silver_rate_per_gram" DOUBLE PRECISION NOT NULL DEFAULT 10,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "zakat_configs_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "zakat_calculations" (
    "id" TEXT NOT NULL,
    "inventory_value" DOUBLE PRECISION NOT NULL,
    "cash_balance" DOUBLE PRECISION NOT NULL,
    "receivables" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "liabilities" DOUBLE PRECISION NOT NULL DEFAULT 0,
    "nisab_threshold" DOUBLE PRECISION NOT NULL,
    "zakatable_amount" DOUBLE PRECISION NOT NULL,
    "zakat_due" DOUBLE PRECISION NOT NULL,
    "gold_rate" DOUBLE PRECISION NOT NULL,
    "store_id" TEXT NOT NULL,
    "calculated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "zakat_calculations_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "zakat_configs_store_id_key" ON "zakat_configs"("store_id");

-- CreateIndex
CREATE INDEX "zakat_calculations_store_id_idx" ON "zakat_calculations"("store_id");
