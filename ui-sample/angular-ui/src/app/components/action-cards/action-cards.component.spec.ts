import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ActionCardsComponent } from './action-cards.component';

describe('ActionCardsComponent', () => {
  let component: ActionCardsComponent;
  let fixture: ComponentFixture<ActionCardsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ActionCardsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ActionCardsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
