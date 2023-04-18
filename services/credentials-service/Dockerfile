FROM node:16
# ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD true

# Install Google Chrome Stable and fonts
# Note: this installs the necessary libs to make the browser work with Puppeteer.
# RUN apt-get update && apt-get install curl gnupg -y \
#   && curl --location --silent https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
#   && sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
#   && apt-get update \
#   && apt-get install google-chrome-stable -y --no-install-recommends \
#   && rm -rf /var/lib/apt/lists/*

#This installs the necessary libs to have wkhtmltopdf tool work
RUN apt-get update \
    && apt-get install -y wkhtmltopdf \
    gconf-service \
    libappindicator1 \
    libgbm-dev \
    libx11-xcb1 \
    libxss1 \
    libxtst6


RUN apt-get install -y \
    fonts-liberation \
    # gconf-service \
    # libappindicator1 \ 
    libasound2 \
    libatk1.0-0 \
    libcairo2 \
    libcups2 \
    libfontconfig1 \ 
    # libgbm-dev \
    libgdk-pixbuf2.0-0 \
    libgtk-3-0 \
    libicu-dev \
    libjpeg-dev \
    libnspr4 \
    libnss3 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libpng-dev \
    libx11-6 \
    # libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxi6 \
    libxrandr2 \
    libxrender1 \
    # libxss1 \
    # libxtst6 \
    xdg-utils

WORKDIR /app
COPY . ./
RUN yarn
EXPOSE 3333
CMD ["yarn", "start"]
