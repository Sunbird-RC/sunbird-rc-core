"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ValidateTemplateService = void 0;
const common_1 = require("@nestjs/common");
const schema_service_1 = require("../schema/schema.service");
let ValidateTemplateService = class ValidateTemplateService {
    constructor(schemaService) {
        this.schemaService = schemaService;
    }
    parseHBS(HBSstr) {
        let HBSfields = HBSstr.match(/{{[{]?(.*?)[}]?}}/g);
        HBSfields.forEach((fieldname) => {
            let len = fieldname.length;
            fieldname = fieldname.slice(2, len - 2);
        });
        HBSfields.sort();
        return HBSfields;
    }
    async verify(template, schemaID) {
        try {
            let HBSfields = this.parseHBS(template);
            let requiredFields = (await this.schemaService.getCredentialSchema({ id: schemaID })).schema["schema"]["required"];
            if (HBSfields.length == requiredFields.length) {
                requiredFields.sort();
                for (let index = 0; index < HBSfields.length; index++) {
                    let field = '{{' + requiredFields[index] + '}}';
                    if (field.localeCompare(HBSfields[index]) === 1 || field.localeCompare(HBSfields[index]) === -1) {
                        return false;
                    }
                }
                return true;
            }
            else {
                console.log("Number of fields in HBS file does not match required field list in schema");
                return false;
            }
        }
        catch (err) {
            throw new common_1.InternalServerErrorException(err);
        }
    }
};
ValidateTemplateService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [schema_service_1.SchemaService])
], ValidateTemplateService);
exports.ValidateTemplateService = ValidateTemplateService;
//# sourceMappingURL=validate-template.service.js.map