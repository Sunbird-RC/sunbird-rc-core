declare const BasicStrategy_base: new (...args: any[]) => any;
export declare class BasicStrategy extends BasicStrategy_base {
    constructor();
    validate: (req: any, username: any, password: any) => Promise<boolean>;
}
export {};
