import { applyDecorators, Header } from '@nestjs/common';
import { RENDER_OUTPUT } from '../enums/renderOutput.enum';

export function RenderRequestHeader(outputType: string) {
    let contentType = 'text/html; charset=utf-8';
    switch (outputType) {
        case RENDER_OUTPUT.HTML:
            contentType = 'text/html; charset=utf-8';
            break;
        case RENDER_OUTPUT.PDF:
            contentType = 'application/pdf';
            break;
    }

    return applyDecorators(Header('Content-Type', contentType));
}
