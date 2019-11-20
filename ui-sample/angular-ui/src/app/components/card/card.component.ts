import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { ICard } from '../../services/interfaces/Card';

@Component({
  selector: 'app-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss']
})
export class CardComponent implements OnInit {

  @Input() data: ICard;
  @Output() clickEvent = new EventEmitter<any>();
  constructor() { }
  color =this.getRandColor();
  ngOnInit() {
  }

  getRandColor() {
    let colors = ["#DD4132", "#727289", "#642F7A", "#A34B25", "#872C6F", "#A34B25", "#8FB339", "#157A7F", "#51504E", "#334A66", "#F7786B","#CE3175","#5B5EA6","#B565A7","#66B7B0"]
    let randNum = Math.floor(Math.random() * 15);
    return colors[randNum];
  }
  public onAction(data, event) {
    this.clickEvent.emit({ 'action': event, 'data': data });
  }
}
