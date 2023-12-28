import { HttpService } from '@nestjs/axios';
import { Injectable, InternalServerErrorException, Logger, NotFoundException } from '@nestjs/common';
import { AxiosError } from '@nestjs/terminus/dist/errors/axios.error';
import { catchError, firstValueFrom } from 'rxjs';
import { PrismaService } from "../utils/prisma.service";
import { uuid } from 'uuidv4';

@Injectable()
export class ContextService {
  private publicEndpoint: string;
  constructor(private readonly httpService: HttpService, private prisma: PrismaService) {
    this.publicEndpoint = process.env.PUBLIC_CONTEXT_ENDPOINT;
  }

  async saveContextAndGetUrl(context: any): Promise<string[]> {
    if(!context) return [];
    if(typeof context === 'string') {
      try {
        const value = JSON.parse(context);
        const url = await this.saveContext(value);
        return [url];
      } catch(err) {
        return [context];
      }
    } else if(Array.isArray(context)) {
      const value = await Promise.all(context.map(d => this.saveContextAndGetUrl(d)
      .then(resp => resp[0])));
      return value;
    } else if(typeof context === 'object') {
      const url = await this.saveContext(context);
      return [url];
    }
    throw new InternalServerErrorException("Unable to process context");
  }

  async saveContext(context: object) {
    let saved = undefined;
    try {
      saved = await this.prisma.context.create({
        data: {
          id: `${uuid()}`,
          context: JSON.stringify(context),
        },
      });
    } catch (err) {
      Logger.error(`Error writing context to database ${err}`);
      throw new InternalServerErrorException('Error writing context to database');
    }
    return `${this.publicEndpoint}/${saved?.id}.json`;
  }

  async getContextById(id: string): Promise<object> {
    let saved = undefined;
    try {
      saved = await this.prisma.context.findUniqueOrThrow({
        where: { id },
      });
    } catch (err) {
      Logger.error(`Error fetching context from database ${err}`);
      throw new NotFoundException('The context not found');
    }
    return JSON.parse(saved.context);
  }

  async resolveContext(context: any[]): Promise<any[]> {
    const results = await Promise.all(context?.map(d => new Promise(async (resolve: (value: Object) => void, reject) => {
      if (d && typeof d === "string") {
        try {
          const value = JSON.parse(d);
          resolve(value);
        } catch(err) {
          if(d.startsWith(this.publicEndpoint)) {
            const contextId = d.substring(this.publicEndpoint.length  + 1, d.length - 5);
            const value = await this.getContextById(contextId);
            resolve(value);
          } else {
            try {
              const { data } = await this.httpService.axiosRef({
                url: d,
                method: `GET`,
              });
              resolve(data);
            } catch(exp) {
              reject(`Unable to fetch context: ${d}`);
            }
            
          }
        }
      } else if(typeof d === "object") {
        resolve(d);
      }
      reject(`Unable to resolve context: ${d}`);
    })));
    return results;
  }

}
