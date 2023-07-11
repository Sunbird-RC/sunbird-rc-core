import { AddTemplateDTO } from './dto/addTemplate.dto';

export const templatePayloadGenerator = (
  schemaID: string,
  schemaVersion: string,
): AddTemplateDTO => {
  return {
    schemaId: schemaID,
    schemaVersion: schemaVersion,
    template:
      "<html lang='en'><head><meta charset='UTF-8' /><meta http-equiv='X-UA-Compatible' content='IE=edge' /><meta name='viewport' content='width=device-width, initial-scale=1.0' /><script src='https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.0/jquery.min.js'></script><link rel='stylesheet' href='style.css' /><title>Certificate</title></head><body><div class='outer-border'><div class='inner-dotted-border'><img class='logo' src='https://www.vid.no/site/assets/files/17244/christ-deemed-to-be-university-vid.png' alt='' /><br /><span class='certification'>CERTIFICATE OF COMPLETION</span><br /><br /><span class='certify'><i>is hereby awarded to</i></span><br /><br /><span class='name'><b>Daniel Vitorrie</b></span><br /><br /><span class='certify'><i>for successfully completing the</i></span><br /><br /><span class='fs-30 diploma'>diploma in Java Developer</span><br /><br /><span class='fs-20 thank'>Thank you for demonstrating the type of character and integrity that inspire others</span><br /><div class='footer'><div class='date'><span class='certify'><i>Awarded: </i></span><br /><span class='fs-20'> xxxxxx</span></div><div class='qr'>{{grade}}, {{programme}}, {{certifyingInstitute}}, {{evaluatingInstitute}}/></div><span class='sign'>Dean, Christ Univeristy</span></div></div></div></body><style>*{margin:1px;padding:0;box-sizing:border-box}.outer-border{width:80vw;padding:10px;text-align:center;border:10px solid #252F50;font-family:'Lato', sans-serif;margin:auto}.inner-dotted-border{padding:10px;text-align:center;border:2px solid #252F50}.logo{width:75px}.certification{font-size:50px;font-weight:bold;color:#252F50;font-family:\"Times New Roman\", Times, serif}.certify{font-size:20px;color:#252F50}.diploma{color:#252F50;font-family:\"Lucida Handwriting\", \"Comic Sans\", cursive}.name{font-size:30px;color:#252F50;border-bottom:1px solid;font-family:\"Lucida Handwriting\", \"Comic Sans\", cursive}.thank{color:#252F50}.fs-30{font-size:30px}.fs-20{font-size:20px}.footer{display:flex;justify-content:space-between;margin:0 4rem}.date{display:flex;align-self:flex-end}.date>.fs-20{font-family:\"Lucida Handwriting\", \"Comic Sans\", cursive}.qr{margin-top:1.25rem;display:flex;justify-content:end;margin-left:0.75rem;margin-right:0.75rem}.sign{border-top:1px solid;align-self:flex-end}</style></html>",
    type: 'Handlebar',
  };
};
