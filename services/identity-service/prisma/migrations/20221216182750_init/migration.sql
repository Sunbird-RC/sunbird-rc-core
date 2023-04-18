-- CreateTable
CREATE TABLE "Identity" (
    "did" TEXT NOT NULL,
    "privateKey" TEXT NOT NULL,

    CONSTRAINT "Identity_pkey" PRIMARY KEY ("did")
);
